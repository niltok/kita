import {createSlice, PayloadAction} from "@reduxjs/toolkit"
import {RootState} from "../store"
import {applyFlatDiff, FlatDiff} from "../utils"

export interface GameState {
    token?: string | null,
    username?: string | null,
    url?: string | null,
    socket?: WebSocket | null
}

const initialState = {} as GameState

export const gameStateSlicer = createSlice({
    name: 'gameState',
    initialState,
    reducers: {
        applyDiff(state, action: PayloadAction<FlatDiff>) {
            applyFlatDiff(state, action.payload)
        },
    }
})

export const {applyDiff} = gameStateSlicer.actions
export const selectGameState = (state: RootState) => state.gameState
