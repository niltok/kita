import {useAppSelector} from "../storeHook"
import Immutable from "immutable"
import {renderDrawables} from "../utils"
import {Container} from "@inlet/react-pixi"
import {useEffect} from "react";
import {sendSocket$} from "../dbus";

export function Star() {
    const {height, width} = useAppSelector(state => state.gameState.windowSize)
    const starDrawables = Immutable.Map(useAppSelector(state =>
        state.seqState["starDrawables"].data
    )).toArray()
    const state = useAppSelector(state => state.gameState.connection.state)
    const { camera } = useAppSelector(state => state.gameState.star)
    useEffect(() => {
        if (state == 'connected') {
            sendSocket$.next({type: "state.seq.require", target: "starDrawables"})
        }
    }, [state])
    return (<Container position={[width / 2 - camera.x, height / 2 - camera.y]} rotation={camera.rotation}>
        {renderDrawables(starDrawables)}
    </Container>)
}
