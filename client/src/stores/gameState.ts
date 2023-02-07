import {createSlice, PayloadAction} from "@reduxjs/toolkit"
import {RootState} from "../store"
import {useAppDispatch} from "../storeHook"
import {applyObjectDiff} from "../utils";
import {UIElement} from "../types/UIElement";

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
        camera: { x: number, y: number, rotation: number },
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
        width: document.body.offsetWidth
    },
    star: {
        camera: { x: 0, y: 0, rotation: 0 }
    },
    uiState: {}
}

export const gameStateSlicer = createSlice({
    name: 'gameState',
    initialState,
    reducers: {
        diffGame(state, action: PayloadAction<Partial<GameState>>) {
            applyObjectDiff(state, action.payload)
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
