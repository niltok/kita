import {createSlice, original, PayloadAction} from "@reduxjs/toolkit"
import {RootState} from "../store"
import {useAppDispatch} from "../storeHook"

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
        camera: { x: number, y: number, rotation: number }
    }
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
    }
}

function applyObjectDiff(obj: any, diff: { [key: string]: any }) {
    console.log(original(obj), diff)
    for (const ptr in diff) {
        const val = diff[ptr]
        if (val === null) delete obj[ptr]
        else if (typeof val == 'object' && typeof obj[ptr] != 'undefined'
            && obj[ptr] != null && typeof original(obj[ptr]) == 'object')
            applyObjectDiff(obj[ptr], diff[ptr])
        else obj[ptr] = diff[ptr]
    }
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
        }
    }
})

export const {diffGame, addAssets} = gameStateSlicer.actions
export const selectGameState = (state: RootState) => state.gameState

export const useDiffGame = () => {
    const dispatch = useAppDispatch()
    return (payload: Partial<GameState>) => dispatch(diffGame(payload))
}
