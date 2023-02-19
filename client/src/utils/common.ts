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
    let keyCode = ''
    if (e.ctrlKey) keyCode += '$'
    if (e.metaKey) keyCode += '#'
    if (e.altKey) keyCode += '@'
    if (e.shiftKey) keyCode += '^'
    keyCode += e.code
    if (e.type == 'keyup') keyCode += '!'
    return keyCode
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
