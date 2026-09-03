(function (global) {
  'use strict';

  var rejectedInputTypes = {
    password: true,
    hidden: true,
    checkbox: true,
    radio: true,
    file: true,
    button: true,
    submit: true,
    reset: true,
    image: true
  };

  function visible(element) {
    return !!element && !element.hidden &&
      (!element.getClientRects || element.getClientRects().length > 0);
  }

  function safe(element) {
    if (!element || element.disabled || !visible(element)) return false;
    var tag = String(element.tagName || '').toUpperCase();
    if (tag === 'INPUT') return !rejectedInputTypes[String(element.type || 'text').toLowerCase()];
    return tag === 'TEXTAREA' || element.isContentEditable === true;
  }

  function active() {
    return safe(document.activeElement) ? document.activeElement : null;
  }

  function emit(element, type, data) {
    var init = { bubbles: true, cancelable: type === 'beforeinput', data: data };
    var EventType = (type === 'beforeinput' || type === 'input') && global.InputEvent
      ? global.InputEvent : global.Event;
    element.dispatchEvent(new EventType(type, init));
  }

  function setNativeValue(element, value) {
    var owner = element;
    var descriptor = null;
    while (owner && !descriptor) {
      owner = Object.getPrototypeOf(owner);
      if (owner) descriptor = Object.getOwnPropertyDescriptor(owner, 'value');
    }
    if (descriptor && descriptor.set) descriptor.set.call(element, value);
    else element.value = value;
  }

  function insert(text) {
    var element = active();
    if (!element || typeof text !== 'string' || text.length === 0) return false;
    emit(element, 'beforeinput', text);
    if (element.isContentEditable) {
      document.execCommand('insertText', false, text);
    } else {
      var start = typeof element.selectionStart === 'number' ? element.selectionStart : element.value.length;
      var end = typeof element.selectionEnd === 'number' ? element.selectionEnd : start;
      setNativeValue(element, element.value.slice(0, start) + text + element.value.slice(end));
      if (typeof element.setSelectionRange === 'function') {
        element.setSelectionRange(start + text.length, start + text.length);
      }
    }
    emit(element, 'input', text);
    emit(element, 'change', text);
    return true;
  }

  function backspace() {
    var element = active();
    if (!element) return false;
    emit(element, 'beforeinput', null);
    if (element.isContentEditable) {
      document.execCommand('delete', false, null);
    } else {
      var end = typeof element.selectionEnd === 'number' ? element.selectionEnd : element.value.length;
      var start = typeof element.selectionStart === 'number' ? element.selectionStart : end;
      if (start === end && start > 0) start -= 1;
      setNativeValue(element, element.value.slice(0, start) + element.value.slice(end));
      if (typeof element.setSelectionRange === 'function') element.setSelectionRange(start, start);
    }
    emit(element, 'input', null);
    emit(element, 'change', null);
    return true;
  }

  function submit() {
    var element = active();
    if (!element) return false;
    var form = element.form || (element.closest && element.closest('form'));
    if (form && typeof form.requestSubmit === 'function') {
      form.requestSubmit();
      return true;
    }
    if (form && typeof form.querySelector === 'function') {
      var button = form.querySelector('button[type="submit"],input[type="submit"],[role="button"][aria-label*="search" i],[role="button"][aria-label*="поиск" i]');
      if (button && !button.disabled && visible(button) && typeof button.click === 'function') {
        button.click();
        return true;
      }
    }
    ['keydown', 'keypress', 'keyup'].forEach(function (type) {
      element.dispatchEvent(new global.KeyboardEvent(type, {
        key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true, cancelable: true
      }));
    });
    return true;
  }

  function notifyFocus() {
    if (global.JmgoWebBridge && typeof global.JmgoWebBridge.onEditableFocus === 'function') {
      global.JmgoWebBridge.onEditableFocus(!!active());
    }
  }

  function install() {
    if (global.__jmgoWebInputInstalled) return;
    global.__jmgoWebInputInstalled = true;
    document.addEventListener('focusin', notifyFocus, true);
    document.addEventListener('focusout', function () { global.setTimeout(notifyFocus, 0); }, true);
    notifyFocus();
  }

  global.JmgoWebInput = {
    install: install,
    insert: insert,
    backspace: backspace,
    submit: submit,
    hasSafeActiveElement: function () { return !!active(); }
  };
})(typeof window !== 'undefined' ? window : globalThis);
