import {useEffect, useState} from "react";
import {Observable} from "rxjs";

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

export function useObservable<T>(obs: Observable<T>, init: T): T {
    const [value, setValue] = useState(init)
    useEffect(() => {
        const sub = obs.subscribe({
            next(val) {
                setValue(val)
            }
        });
        return () => sub.unsubscribe()
    })
    return value
}
