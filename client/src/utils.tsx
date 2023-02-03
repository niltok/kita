import {useEffect, useState} from "react"
import {Observable} from "rxjs"
import {keyEvents$} from "./dbus";
import {useDiffGame} from "./stores/gameState";

export const delay = (time: number) => {
    return new Promise(resolve => setTimeout(resolve, time))
}

export function useAsyncEffect(effect: () => Promise<void | (() => void)>, dependencies?: any[]) {
    return useEffect(() => {
        const cleanupPromise = effect()
        return () => { cleanupPromise.then(cleanup => cleanup && cleanup()) }
    }, dependencies)
}

export function useRefresh(): [boolean, () => void] {
    const [flag, setFlag] = useState(false)
    return [flag, () => setFlag(flag => !flag)]
}

export function useSubscribe<T>(obs: Observable<T>, callback: (val: T) => void) {
    useEffect(() => {
        const sub = obs.subscribe({
            next(val) { callback(val) }
        });
        return () => sub.unsubscribe()
    }, [obs, callback])
}

export function useObservable<T>(obs: Observable<T>, init: T): T {
    const [value, setValue] = useState(init)
    useSubscribe(obs, setValue);
    return value
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

export function useKeyboard() {
    useEffect(() => {
        function handleKeyEvent(e: KeyboardEvent) {
            e.preventDefault()
            if (e.repeat) return
            keyEvents$.next(e)
        }

        document.body.addEventListener('keydown', handleKeyEvent)
        document.body.addEventListener('keyup', handleKeyEvent)
        return () => {
            document.body.removeEventListener('keydown', handleKeyEvent)
            document.body.removeEventListener('keyup', handleKeyEvent)
        }
    }, [])
}

export function useWindowSize() {
    const diffGame = useDiffGame()
    useEffect(() => {
        diffGame({
            windowSize: {
                height: document.body.clientHeight,
                width: document.body.clientWidth
            }
        })
        const listener = () => {
            diffGame({
                windowSize: {
                    height: document.body.clientHeight,
                    width: document.body.clientWidth
                }
            })
        }
        window.addEventListener('resize', listener)
        return () => window.removeEventListener('resize', listener)
    }, [])
}