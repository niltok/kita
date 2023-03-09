import Socket from "./Socket"
import {Stage} from "./Stage"
import {renderUI} from "./RenderUI";
import {useAppSelector} from "../storeHook";
import {useEffect} from "react";
import {useDiffGame} from "../stores/gameState";

export function Game() {
    const ui = useAppSelector(state => state.gameState.ui)
    const differ = useDiffGame()
    useEffect(() => {
        differ({ ui: undefined })
    }, [])
    return (<Socket>
        <div className={"absolute fullscreen"}>{renderUI(ui)}</div>
        <Stage/>
    </Socket>)
}