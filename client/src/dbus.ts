import {Subject} from "rxjs"
import {SeqDrawable} from "./components/Stage"
import {StateEvent} from "./types/Worker"

export const sendSocket$ = new Subject<object>()
export const setPage$ = new Subject<string>()
export const keyEvents$ = new Subject<KeyboardEvent>()
export const mouseEvents$ = new Subject<PointerEvent>()
export const seqDrawables$ = new Subject<SeqDrawable>()
export const workerCommon$ = new Subject<StateEvent>()
export const rendererEvent$ = new Subject<any>()