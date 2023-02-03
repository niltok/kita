import {Subject} from "rxjs"
import {SeqDrawable} from "./components/Stage"

export const sendSocket$ = new Subject<object>()
export const setPage$ = new Subject<string>()
export const keyEvents$ = new Subject<KeyboardEvent>()
export const seqDrawables$ = new Subject<SeqDrawable>()
