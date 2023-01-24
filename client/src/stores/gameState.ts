import {createSlice, PayloadAction} from "@reduxjs/toolkit"
import {RootState} from "../store"
import pointer from "json-pointer";

export interface GameState {
    token: string | null,
    username: string | null,
    url: string | null,
    socket: WebSocket | null
    page: "load" | "map"
}

const initialState: GameState = {
    token: null,
    username: null,
    url: null,
    socket: null,
    page: "load",
}

function applyFlatDiff(obj: object, diff: { [key: string]: any }) {
    for (const ptr in diff) {
        const val = diff[ptr]
        if (val === undefined || val === null) pointer.remove(obj, ptr)
        else pointer(obj, ptr, diff[ptr])
    }
}

export const gameStateSlicer = createSlice({
    name: 'gameState',
    initialState,
    reducers: {
        diffGame(state, action: PayloadAction<{ [key: string]: any }>) {
            applyFlatDiff(state, action.payload)
        },
    }
})

export const {diffGame} = gameStateSlicer.actions
export const selectGameState = (state: RootState) => state.gameState
