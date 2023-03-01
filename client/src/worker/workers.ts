import Preprocessor from './preprocessor.worker?worker'
import Renderer from './renderer.worker?worker'
import {rendererEvent$, workerCommon$} from "../dbus"
import {ModifyEvent, StateEvent} from "../types/Worker"

export const preprocessor = new Preprocessor()
export const renderer = new Renderer()

workerCommon$.subscribe({
    next(state) {
        // preprocessor.postMessage(state)
        renderer.postMessage(state)
    }
})

// preprocessor.onmessage = (e: MessageEvent<ModifyEvent>) => renderer.postMessage({
//     ...e.data,
//     type: 'preprocessed'
// } as StateEvent)

renderer.onmessage = e => rendererEvent$.next(e.data)
