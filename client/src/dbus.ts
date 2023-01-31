import {Subject} from "rxjs"

export const sendSocket$ = new Subject<object>()
export const setPage$ = new Subject<string>()
export const keyEvents$ = new Subject<KeyboardEvent>()