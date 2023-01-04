import pointer from 'json-pointer'
import {useEffect, useState} from "react";

export const delay = (time: number) => {
    return new Promise(resolve => setTimeout(resolve, time))
}

export type FlatDiff = {
    [key: string]: any
}

export function applyFlatDiff(obj: object, diff: FlatDiff) {
    for (const ptr in diff) {
        const val = diff[ptr]
        if (val === undefined) pointer.remove(obj, ptr)
        else pointer(obj, ptr, diff[ptr])
    }
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
