import {createSlice, PayloadAction} from "@reduxjs/toolkit"
import {useAppDispatch} from "../storeHook"
import {applyReduxDiff} from "../utils/react"

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
        ui?: string
    }
    ui?: string
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
        camera: { x: 0, y: 0, rotation: 0 }
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
