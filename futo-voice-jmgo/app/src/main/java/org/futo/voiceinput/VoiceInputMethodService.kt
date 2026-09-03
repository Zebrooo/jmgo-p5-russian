package org.futo.voiceinput

import android.content.Context
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.futo.voiceinput.migration.scheduleModelMigrationJob
import org.futo.voiceinput.settings.pages.ConditionalUnpaidNoticeInVoiceInputWindow
import org.futo.voiceinput.theme.UixThemeAuto
import org.futo.voiceinput.updates.scheduleUpdateCheckingJob
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val SupportsNavbarExtension = Build.VERSION.SDK_INT >= 28

@Composable
fun navBarHeight(): Dp = with(LocalDensity.current) {
    if(SupportsNavbarExtension) {
        WindowInsets.systemBars.getBottom(this).toDp()
    } else {
        0.dp
    }
}


@Composable
fun RecognizerInputMethodWindow(switchBack: (() -> Unit)? = null, allowClick: Boolean = false, onPauseVAD: (Boolean) -> Unit = { }, onFinish: () -> Unit = { }, content: @Composable ColumnScope.() -> Unit) {
    UixThemeAuto(false) {
        Surface(
            modifier = Modifier
                .recognizerSurfaceClickable(disabled = !allowClick, onPauseVAD = onPauseVAD, onFinish = onFinish)
                .fillMaxWidth()
                .wrapContentHeight(),
            color = MaterialTheme.colorScheme.surface
        ) {
            val icon = painterResource(id = R.drawable.futo_o)
            val bgIconTint = MaterialTheme.colorScheme.outline

            Column(
                modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 64.dp).drawBehind {
                    with(icon) {
                        translate(left = -icon.intrinsicSize.width/2, top = -icon.intrinsicSize.height/2) {
                            translate(left = size.width / 3, top = size.height / 2) {
                                scale(scaleX = 1.3f, scaleY = 1.3f) {
                                    draw(icon.intrinsicSize, colorFilter = ColorFilter.tint(bgIconTint))
                                }

                            }
                        }
                    }
                }
            ) {

                val context = LocalContext.current
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.align(Alignment.CenterStart)) {
                        ConditionalUnpaidNoticeInVoiceInputWindow(switchBack)
                    }

                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        if (switchBack != null) {
                            IconButton(
                                onClick = switchBack
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                Box(modifier = Modifier.padding(12.dp)) {
                    
                }

                content()
                Spacer(Modifier.height(navBarHeight()))
            }
        }
    }
}


@Preview
@Composable
fun RecognizeIMELoadingPreview() {
    RecognizerInputMethodWindow(switchBack = { }) {
        RecognizeLoadingCircle()
    }
}

@Preview
@Composable
fun PreviewRecognizeViewLoadedIME() {
    RecognizerInputMethodWindow(switchBack = { }) {
        InnerRecognize()
    }
}
@Preview
@Composable
fun PreviewRecognizeViewNoMicIME() {
    RecognizerInputMethodWindow(switchBack = { }) {
        RecognizeMicError(openSettings = { })
    }
}


val punctuationChars = setOf('!', '?', '.', ',')
class VoiceInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private val isJmgoBuild: Boolean
        get() = packageName == "org.futo.voiceinput.jmgo"

    private val jmgoVoiceSession = JmgoVoiceSession()
    private val jmgoKeyboardLanguage = mutableStateOf(JmgoKeyboardLanguage.RUSSIAN)
    private val jmgoKeyboardSelection = mutableStateOf(JmgoKeyboardSelection(0, 0))
    private var hasActiveInput = false

    private val mSavedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = mSavedStateRegistryController.savedStateRegistry

    private val mLifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle
        get() = mLifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore
        get() = store

    private fun handleLifecycleEvent(event: Lifecycle.Event) =
        mLifecycleRegistry.handleLifecycleEvent(event)

    private val inputMethodManager: InputMethodManager
        get() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    override fun onCreate() {
        super.onCreate()
        mSavedStateRegistryController.performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        scheduleUpdateCheckingJob(applicationContext)
        scheduleModelMigrationJob(applicationContext)

        if (isJmgoBuild) {
            lifecycle.coroutineScope.launch(Dispatchers.Default) {
                val preloadStartedAt = SystemClock.elapsedRealtime()
                runCatching {
                    JmgoVoskEngine.prepare(File(filesDir, JmgoVoskPolicy.MODEL_DIRECTORY))
                    Log.i(
                        "JmgoVoiceInput",
                        "Vosk recognizer preloaded in ${SystemClock.elapsedRealtime() - preloadStartedAt}ms",
                    )
                }.onFailure { error ->
                    Log.e("JmgoVoiceInput", "Vosk preload failed", error)
                }
            }
        }
    }

    private val recognizer = object : RecognizerView() {
        override val context: Context
            get() = this@VoiceInputMethodService
        override val lifecycleScope: LifecycleCoroutineScope
            get() = this@VoiceInputMethodService.lifecycle.coroutineScope

        private val currentContent: MutableState<@Composable () -> Unit> = mutableStateOf( { } )
        override fun setContent(content: @Composable () -> Unit) {
            currentContent.value = content
            composeView?.setContent { content() }
        }

        fun refreshContent() {
            composeView?.setContent { currentContent.value() }
        }

        override fun onCancel() {
            if (isJmgoBuild) {
                completeJmgoRecognition()
                return
            }

            needsInitialization = true
            reset()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToPreviousInputMethod()
            } else {
                inputMethodManager.switchToLastInputMethod(window.window!!.attributes.token)
            }
        }

        var prevText: CharSequence? = null
        var nextText: CharSequence? = null
        override fun decodingStarted() {
            this@VoiceInputMethodService.currentInputConnection.also {
                prevText = it.getTextBeforeCursor(1, 0)
                nextText = it.getTextAfterCursor(1, 0)
            }
        }

        override fun sendResult(result: String) {
            if (isJmgoBuild) Log.i("JmgoVoiceInput", "sendResult chars=${result.length}")
            this@VoiceInputMethodService.currentInputConnection.also {
                var modifiedResult = result

                // Insert space automatically if ended at punctuation
                // TODO: Could send text before cursor as whisper prompt

                if(!prevText.isNullOrBlank()) {
                    val lastChar = prevText?.last()

                    if (punctuationChars.contains(lastChar)) {
                        modifiedResult = " $result"
                    }
                }

                /*
                if(!nextText.isNullOrBlank()) {
                    val oldPunctuation = nextText?.first()
                    val newPunctuation = result.last()

                    if (punctuationChars.contains(oldPunctuation) && punctuationChars.contains(newPunctuation)) {
                        it.deleteSurroundingText(0, 1)
                    }
                }
                */

                val committed = it.commitText(modifiedResult, 1)
                val composingFinished = it.finishComposingText()
                if (isJmgoBuild) {
                    Log.i(
                        "JmgoVoiceInput",
                        "commitText=$committed finishComposingText=$composingFinished connection=${currentInputConnection != null}",
                    )
                }

                if (isJmgoBuild) {
                    submitCurrentInput(it)
                }
            }

            if (isJmgoBuild) {
                completeJmgoRecognition()
            } else {
                onCancel()
            }
        }

        override fun sendPartialResult(result: String): Boolean {
            if(this@VoiceInputMethodService.currentInputConnection != null) {
                this@VoiceInputMethodService.currentInputConnection.setComposingText(result, 1)
                return true
            } else {
                return false
            }
        }

        override fun requestPermission() {
            // We can't ask for permission from a service
            // TODO: We could launch an activity and request it that way

            permissionResultRejected()
        }

        @Composable
        override fun Window(onClose: () -> Unit, allowClick: Boolean, onPauseVAD: (Boolean) -> Unit, onFinish: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
            RecognizerInputMethodWindow(switchBack = onClose, onPauseVAD = onPauseVAD, onFinish = onFinish, allowClick = allowClick) {
                content()
            }
        }
    }

    private fun setOwners() {
        val decorView = window.window?.decorView
        if (decorView?.findViewTreeLifecycleOwner() == null) {
            decorView?.setViewTreeLifecycleOwner(this)
        }
        if (decorView?.findViewTreeViewModelStoreOwner() == null) {
            decorView?.setViewTreeViewModelStoreOwner(this)
        }
        if (decorView?.findViewTreeSavedStateRegistryOwner() == null) {
            decorView?.setViewTreeSavedStateRegistryOwner(this)
        }

        window.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private var composeView: ComposeView? = null

    private fun showJmgoIdleInput() {
        composeView?.setContent {
            JmgoKeyboard(
                language = jmgoKeyboardLanguage.value,
                selection = jmgoKeyboardSelection.value,
                onLanguage = ::switchJmgoKeyboardLanguage,
                onNumbers = { setJmgoKeyboardLanguage(JmgoKeyboardLanguage.NUMBERS) },
                onText = { text -> currentInputConnection?.commitText(text, 1) },
                onSpace = { currentInputConnection?.commitText(" ", 1) },
                onBackspace = ::backspaceJmgoInput,
                onHide = { requestHideSelf(0) },
                onEnter = { currentInputConnection?.let(::submitCurrentInput) },
            )
        }
    }

    private fun handleJmgoKeyboardSignal(action: String?): Boolean {
        if (action !in JmgoKeyboardSignal.actions) return false
        if (!hasActiveInput || currentInputConnection == null || !isInputViewShown) return true

        val rows = JmgoKeyboardLayout.rows(jmgoKeyboardLanguage.value)
        val direction = JmgoKeyboardSignal.directionForAction(action)
        if (direction != null) {
            jmgoKeyboardSelection.value = JmgoKeyboardNavigation.move(
                rows,
                jmgoKeyboardSelection.value,
                direction,
            )
        } else if (action == JmgoKeyboardSignal.ACTION_SELECT) {
            activateJmgoKeyboardSelection(rows)
        }
        return true
    }

    private fun activateJmgoKeyboardSelection(rows: List<List<String>>) {
        when (JmgoKeyboardNavigation.keyAt(rows, jmgoKeyboardSelection.value)) {
            JmgoKeyboardKey.TEXT -> JmgoKeyboardNavigation
                .textAt(rows, jmgoKeyboardSelection.value)
                ?.let { currentInputConnection?.commitText(it, 1) }
            JmgoKeyboardKey.LANGUAGE -> switchJmgoKeyboardLanguage()
            JmgoKeyboardKey.NUMBERS -> setJmgoKeyboardLanguage(JmgoKeyboardLanguage.NUMBERS)
            JmgoKeyboardKey.SPACE -> currentInputConnection?.commitText(" ", 1)
            JmgoKeyboardKey.BACKSPACE -> backspaceJmgoInput()
            JmgoKeyboardKey.HIDE -> requestHideSelf(0)
            JmgoKeyboardKey.ENTER -> currentInputConnection?.let(::submitCurrentInput)
        }
    }

    private fun switchJmgoKeyboardLanguage() {
        setJmgoKeyboardLanguage(
            if (jmgoKeyboardLanguage.value == JmgoKeyboardLanguage.RUSSIAN) {
                JmgoKeyboardLanguage.ENGLISH
            } else {
                JmgoKeyboardLanguage.RUSSIAN
            },
        )
    }

    private fun setJmgoKeyboardLanguage(language: JmgoKeyboardLanguage) {
        jmgoKeyboardLanguage.value = language
        jmgoKeyboardSelection.value = JmgoKeyboardNavigation.clamp(
            JmgoKeyboardLayout.rows(language),
            jmgoKeyboardSelection.value,
        )
    }

    private fun backspaceJmgoInput() {
        currentInputConnection?.let { connection ->
            if (connection.getSelectedText(0).isNullOrEmpty()) {
                connection.deleteSurroundingText(1, 0)
            } else {
                connection.commitText("", 1)
            }
        }
    }

    private fun submitCurrentInput(connection: InputConnection) {
        val imeOptions = currentInputEditorInfo?.imeOptions ?: 0
        if (JmgoVoiceSignal.shouldSendEnterKey(imeOptions)) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        } else {
            JmgoVoiceSignal.submitAction(imeOptions)?.let(connection::performEditorAction)
                ?: connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun startJmgoRecognition() {
        if (!hasActiveInput || currentInputConnection == null) {
            jmgoVoiceSession.reset()
            return
        }

        requestShowSelf(InputMethodManager.SHOW_IMPLICIT)
        recognizer.reset()
        recognizer.init()
    }

    private fun finishJmgoRecognition() {
        if (recognizer.isRecording()) {
            recognizer.finishRecognizerIfRecording()
        } else {
            completeJmgoRecognition()
        }
    }

    private fun completeJmgoRecognition() {
        jmgoVoiceSession.reset()
        recognizer.reset()
        showJmgoIdleInput()
        requestHideSelf(0)
    }

    override fun onEvaluateInputViewShown(): Boolean =
        JmgoImeSurfacePolicy.shouldShowInputView(isJmgoBuild) &&
            super.onEvaluateInputViewShown()

    override fun onCreateInputView(): View {
        // The input view is the main view where the user inputs text via keyclicks, handwriting,
        // gestures, or in this case there is a voice input menu.
        composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setParentCompositionContext(null)

            this@VoiceInputMethodService.setOwners()
        }

        updateNavigationBarVisibility()
        return composeView!!
    }

    override fun onCreateCandidatesView(): View? {
        // The candidates view shows potential word corrections or suggestions for the user to select.
        // Return null, as the voice input does not need this.
        return null
    }

    private fun updateNavigationBarVisibility() {
        if(SupportsNavbarExtension) {
            window.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    private var needsInitialization = true
    override fun onStartInput(info: EditorInfo, restarting: Boolean) {
        super.onStartInput(info, restarting)
        if (isJmgoBuild) {
            hasActiveInput = true
            jmgoVoiceSession.reset()
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        when (info.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER -> {
                // number
            }
            InputType.TYPE_CLASS_DATETIME -> {
                // date time ??
            }
            InputType.TYPE_CLASS_PHONE -> {
                // phone number
                // could add whisper prompt like "My phone number is "
            }
            InputType.TYPE_CLASS_TEXT -> {
                // text :)
                if(info.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                    // ...
                }
            }
        }

        if (isJmgoBuild) {
            if (!jmgoVoiceSession.isActive) showJmgoIdleInput()
        } else if(needsInitialization) {
            needsInitialization = false
            recognizer.reset()
            recognizer.init()
        } else {
            println("Continuing recording, likely due to landscape/portrait switch")
            recognizer.refreshContent()
        }
        // TODO: Idle state
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        println("Finish input view")
        recognizer.reset()
        jmgoVoiceSession.reset()

        needsInitialization = true
    }

    override fun onFinishInput() {
        hasActiveInput = false
        jmgoVoiceSession.reset()
        recognizer.reset()
        super.onFinishInput()
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateNavigationBarVisibility()
    }

    override fun onDestroy() {
        super.onDestroy()

        println("Destroy")
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
