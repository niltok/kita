export type KeyType = string | { action: string, value: number } | { type: string, [key: string]: any };

/// 前缀（注意顺序）：$(Ctrl), #(Meta), @(Alt), ^(Shift)
//
// 后缀：!(keyup)
//
// 举例：$^Digit1! 表示Ctrl+Shift+1这个组合键抬起
export const keyMapper: { [key: string]: KeyType } = {
    "KeyW": {action: "up", value: 2},
    "KeyW!": "up",
    "KeyS": {action: "down", value: 2},
    "KeyS!": "down",
    "KeyA": {action: "left", value: 2},
    "KeyA!": "left",
    "KeyD": {action: "right", value: 2},
    "KeyD!": "right",
    "KeyM": {type: "page.toggle", page: "starMap"},
    "KeyK": {type: "page.toggle", page: "techTrainer"},
    "KeyE": {type: "page.toggle", page: "cargoHold"},
    "KeyC": {type: "page.toggle", page: "shipEquip"},
    "$KeyP": {type: "page.toggle", page: "adminPanel"},
    "Space": {action: "jumpOrFly", value: 3},
    "Space!": "jumpOrFly",
    "MouseLeft": {action: "shot", value: 3},
    "MouseLeft!": "shot",
    // "KeyT": {type: "page.toggle", page: "transfer"},
    "Digit1": "prevWeapon",
    "Digit2": "nextWeapon",
}
