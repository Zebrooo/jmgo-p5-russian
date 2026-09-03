const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

class FakeEvent {
  constructor(type, init = {}) {
    this.type = type;
    Object.assign(this, init);
  }
}

class FakeInput {
  constructor(type = 'text') {
    this.tagName = 'INPUT';
    this.type = type;
    this.disabled = false;
    this.hidden = false;
    this.isContentEditable = false;
    this.events = [];
    this._value = '';
    this.form = null;
  }
  get value() { return this._value; }
  set value(next) { this._value = String(next); }
  getClientRects() { return [{ width: 100, height: 20 }]; }
  dispatchEvent(event) { this.events.push(event); return true; }
  focus() {}
}

class FakeContentEditable {
  constructor() {
    this.tagName = 'DIV';
    this.disabled = false;
    this.hidden = false;
    this.isContentEditable = true;
    this.textContent = '';
    this.events = [];
    this.form = null;
  }
  getClientRects() { return [{ width: 100, height: 20 }]; }
  dispatchEvent(event) { this.events.push(event); return true; }
  focus() {}
}

function loadRuntime(activeElement) {
  const listeners = {};
  const document = {
    activeElement,
    addEventListener(type, listener) { listeners[type] = listener; },
    execCommand(command, _ui, value) {
      if (command === 'insertText') activeElement.textContent += value;
      if (command === 'delete') activeElement.textContent = activeElement.textContent.slice(0, -1);
      return true;
    },
  };
  const context = {
    document,
    Event: FakeEvent,
    InputEvent: FakeEvent,
    KeyboardEvent: FakeEvent,
    setTimeout,
    clearTimeout,
  };
  context.window = context;
  const sourcePath = path.resolve(__dirname, '../../main/assets/jmgo-web-input.js');
  vm.runInNewContext(fs.readFileSync(sourcePath, 'utf8'), context, { filename: sourcePath });
  return context.JmgoWebInput;
}

test('rejects password and disabled inputs', () => {
  const password = new FakeInput('password');
  assert.equal(loadRuntime(password).hasSafeActiveElement(), false);
  const disabled = new FakeInput();
  disabled.disabled = true;
  assert.equal(loadRuntime(disabled).hasSafeActiveElement(), false);
});

test('inserts through the native value setter and emits observable events', () => {
  const input = new FakeInput();
  const runtime = loadRuntime(input);
  assert.equal(runtime.insert('матрица'), true);
  assert.equal(input.value, 'матрица');
  assert.deepEqual(input.events.map(event => event.type), ['beforeinput', 'input', 'change']);
});

test('inserts and deletes content-editable text', () => {
  const editable = new FakeContentEditable();
  const runtime = loadRuntime(editable);
  assert.equal(runtime.insert('кино'), true);
  assert.equal(runtime.backspace(), true);
  assert.equal(editable.textContent, 'кин');
});

test('submits the owning form with requestSubmit', () => {
  const input = new FakeInput();
  let submitted = 0;
  input.form = { requestSubmit() { submitted += 1; } };
  assert.equal(loadRuntime(input).submit(), true);
  assert.equal(submitted, 1);
});

test('clicks a form submit control when requestSubmit is unavailable', () => {
  const input = new FakeInput();
  let clicked = 0;
  input.form = {
    querySelector() { return { disabled: false, hidden: false, getClientRects: () => [{}], click: () => { clicked += 1; } }; },
  };
  assert.equal(loadRuntime(input).submit(), true);
  assert.equal(clicked, 1);
});

test('dispatches Enter when no form exists', () => {
  const input = new FakeInput();
  assert.equal(loadRuntime(input).submit(), true);
  assert.deepEqual(input.events.slice(-3).map(event => event.type), ['keydown', 'keypress', 'keyup']);
  assert.equal(input.events.at(-1).key, 'Enter');
});
