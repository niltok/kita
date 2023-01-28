import {useAppSelector} from "../storeHook"
import Immutable from "immutable"
import {renderDrawables} from "../utils"
import {Container} from "@inlet/react-pixi"
import {useEffect} from "react";
import {sendSocket$} from "../dbus";

export function Star() {
    const starDrawables = Immutable.Map(useAppSelector(state =>
        state.seqState["starDrawables"].data
    )).toArray()
    const socket = useAppSelector(state => state.gameState.socket)
    useEffect(() => {
        if (socket) sendSocket$.next({ type: "state.seq.require", target: "starDrawables" })
    }, [socket])
    return (<>{renderDrawables(starDrawables)}</>)
}
