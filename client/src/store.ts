import {configureStore} from "@reduxjs/toolkit"
import {gameStateSlicer} from "./stores/gameState"
import {seqStateSlicer} from "./stores/seqState"

export const store = configureStore({
    reducer: {
        gameState: gameStateSlicer.reducer,
        seqState: seqStateSlicer.reducer
    }
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
