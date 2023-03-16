export type KeyType = string | { action: string, value: number } | { type: string, [key: string]: any };

export const keyMapper: { [key: string]: KeyType } = {
    "KeyW": {action: "up", value: 2},
    "KeyW!": "up",
    "KeyS": {action: "down", value: 2},
    "KeyS!": "down",
    "KeyA": {action: "left", value: 2},
    "KeyA!": "left",
    "KeyD": {action: "right", value: 2},
    "KeyD!": "right",
    "KeyM": {type: "starMap.toggle"},
    "KeyK": {type: "techTrainer.toggle"},
    "Space": "jump",
    "Enter": "shot"
}
