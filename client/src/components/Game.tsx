import Socket from "./Socket"
import {Stage} from "./Stage"
import {RenderUI} from "./RenderUI"
import {useAppSelector} from "../storeHook"
import {useEffect} from "react"
import {useDiffGame} from "../stores/gameState"
import './Game.css'

export function Game() {
    const ui = useAppSelector(state => state.gameState.ui)
    const starUI = useAppSelector(state => state.gameState.star.ui)
    const differ = useDiffGame()
    useEffect(() => {
        differ({ ui: undefined })
    }, [])
    return (<Socket>
        <div className={"absolute fullscreen pointer-pass"}>
            {ui?.children?.map(e => <RenderUI elem={e}/>)}
            {starUI?.children?.map(e => <RenderUI elem={e}/>)}
        </div>
        <Stage/>
    </Socket>)
}