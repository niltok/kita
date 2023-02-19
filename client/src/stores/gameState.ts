import {createSlice, PayloadAction} from "@reduxjs/toolkit"
import {RootState} from "../store"
import {useAppDispatch} from "../storeHook"
import {UIElement} from "../types/UIElement";
import {applyReduxDiff} from "../utils/react";

export interface GameState {
    server: {
        token: string,
        username: string,
        url: string
    } | null
    connection: { state: 'uninitialized' | 'connecting' | 'connected' | 'failed' }
    assets: any
    windowSize: { height: number, width: number },
    star: {
        camera: { x: number, y: number },
        ui?: UIElement
    }
    ui?: UIElement
    uiState: { [key: string]: any }
}

const initialState: GameState = {
    server: null,
    assets: {},
    connection: { state: 'uninitialized' },
    windowSize: {
        height: document.body.clientHeight,
        width: document.body.clientWidth
    },
    star: {
        camera: { x: 0, y: 0 }
    },
    uiState: {}
}

export const gameStateSlicer = createSlice({
    name: 'gameState',
    initialState,
    reducers: {
        diffGame(state, action: PayloadAction<Partial<GameState>>) {
            applyReduxDiff(state, action.payload)
        },
        addAssets(state, action: PayloadAction<{ name: string, bundle: any }>) {
            state.assets[action.payload.name] = action.payload.bundle
        },

    }
})

export const {diffGame, addAssets} = gameStateSlicer.actions

export const useDiffGame = () => {
    const dispatch = useAppDispatch()
    return (payload: Partial<GameState>) => dispatch(diffGame(payload))
}
