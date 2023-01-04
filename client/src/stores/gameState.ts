import {createSlice, PayloadAction} from "@reduxjs/toolkit"
import {RootState} from "../store"
import {applyFlatDiff, FlatDiff} from "../utils"

export interface GameState {
    token?: string,
    username?: string,
    url?: string,
    socket?: WebSocket
}

const initialState = {} as GameState

export const gameStateSlicer = createSlice({
    name: 'gameState',
    initialState,
    reducers: {
        applyDiff(state, action: PayloadAction<FlatDiff>) {
            applyFlatDiff(state, action.payload)
        },
        setToken(state, action: PayloadAction<string>) {
            state.token = action.payload
        },
        setUsername(state, action: PayloadAction<string>) {
            state.username = action.payload
        },
        setURL(state, action: PayloadAction<string>) {
            state.url = action.payload
        },
    }
})

export const {applyDiff, setToken, setUsername, setURL} = gameStateSlicer.actions
export const selectGameState = (state: RootState) => state.gameState
