import {configureStore} from "@reduxjs/toolkit"
import {gameStateSlicer} from "./stores/gameState"

export const store = configureStore({
    reducer: {
        gameState: gameStateSlicer.reducer
    },
    middleware: getDefaultMiddleware => getDefaultMiddleware({
        serializableCheck: false
    })
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
