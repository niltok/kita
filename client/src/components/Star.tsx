import {useAppSelector} from "../storeHook"
import Immutable from "immutable"
import {getKeyCode, renderDrawables, useSubscribe} from "../utils"
import {Container} from "@inlet/react-pixi"
import {useEffect} from "react";
import {keyEvents$, sendSocket$} from "../dbus";

const keyMapper = {
    "KeyW": {action: "up", value: 2},
    "KeyW!": "up",
    "KeyS": {action: "down", value: 2},
    "KeyS!": "down",
    "KeyA": {action: "left", value: 2},
    "KeyA!": "left",
    "KeyD": {action: "right", value: 2},
    "KeyD!": "right",
}

function handleKeyEvent(e: KeyboardEvent, mapper: { [key: string]: string | {action: string, value: number} }) {
    const action = mapper[getKeyCode(e)]
    if (typeof action == 'undefined') return
    if (typeof action == 'string')
        sendSocket$.next({
            type: "star.operate.key",
            action
        })
    else if (typeof action == 'object')
        sendSocket$.next({
            type: "star.operate.key",
            ...action
        })
}

export function Star() {
    const {height, width} = useAppSelector(state => state.gameState.windowSize)
    const starDrawables = Immutable.Map(useAppSelector(state =>
        state.seqState["starDrawables"].data
    )).toArray().map(([, drawable]) => drawable)
    const state = useAppSelector(state => state.gameState.connection.state)
    const { camera } = useAppSelector(state => state.gameState.star)
    useEffect(() => {
        if (state == 'connected') {
            sendSocket$.next({type: "state.seq.require", target: "starDrawables"})
        }
    }, [state])
    useSubscribe(keyEvents$, e => handleKeyEvent(e, keyMapper))
    const assets = useAppSelector(state => state.gameState.assets)
    return (<Container position={[width / 2 + camera.x, height / 2 + camera.y]} rotation={camera.rotation}>
        {renderDrawables(starDrawables, assets)}
    </Container>)
}
