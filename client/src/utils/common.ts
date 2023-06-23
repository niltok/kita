export const FPS = 60

export const delay = (time: number) => {
    return new Promise(resolve => setTimeout(resolve, time))
}

/// 前缀（注意顺序）：$(Ctrl), #(Meta), @(Alt), ^(Shift)
//
// 后缀：!(keyup)
//
// 举例：$^Digit1! 表示Ctrl+Shift+1这个组合键抬起
export function getKeyCode(e: KeyboardEvent) {
    let code = ''
    if (e.ctrlKey) code += '$'
    if (e.metaKey) code += '#'
    if (e.altKey) code += '@'
    if (e.shiftKey) code += '^'
    code += e.code
    if (e.type == 'keyup') code += '!'
    return code
}

export function getMouseCode(e: PointerEvent | WheelEvent) {
    if (e.type == 'wheel') {
        let dis = (e as WheelEvent).deltaY
        if (dis < 0) return 'WheelUp'
        if (dis > 0) return 'WheelDown'
        return ''
    }
    if (e.type == 'pointermove') return ''
    let code = '';
    if (e.ctrlKey) code += '$'
    if (e.metaKey) code += '#'
    if (e.altKey) code += '@'
    if (e.shiftKey) code += '^'
    if (e.button == 0) code += 'MouseLeft'
    if (e.button == 1) code += 'MouseMiddle'
    if (e.button == 2) code += 'MouseRight'
    if (e.button == 3) code += 'MouseX1'
    if (e.button == 4) code += 'MouseX2'
    if (e.type == 'pointerup' || e.type == 'pointercancel') code += '!'
    return code
}

export type Pretty<T> = {
    [key in keyof T]: T[key]
}

export function subtractSet<T>(a: Set<T>, b: Set<T>) {
    const res: T[] = []
    a.forEach(v => {
        if (!b.has(v)) res.push(v)
    })
    return res
}

export function applyObjectDiff(obj: any, diff: { [key: string]: any }) {
    if (Array.isArray(obj) && Array.isArray(diff)) {
        while (diff.length < obj.length) obj.pop()
    }
    for (const ptr in diff) {
        const val = diff[ptr]
        if (val === null) delete obj[ptr]
        else {
            let elem = obj[ptr];
            if (typeof val == 'object' && typeof elem == 'object')
                applyObjectDiff(elem, val)
            else obj[ptr] = val
        }
    }
}
