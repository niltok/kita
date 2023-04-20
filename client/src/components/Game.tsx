import Socket from "./Socket"
import {Stage} from "./Stage"
import {RenderUI} from "./RenderUI"
import {useAppSelector} from "../storeHook"
import {useEffect, useMemo} from "react"
import {useDiffGame} from "../stores/gameState"
import './Game.css'
import {UIElement} from "../types/UIElement";

export function Game() {
    const uis = useAppSelector(state => state.gameState.ui)
    const ui = useMemo(() => uis ? JSON.parse(uis) as UIElement : undefined, [uis])
    const starUIs = useAppSelector(state => state.gameState.star.ui)
    const starUI = useMemo(() => starUIs ? JSON.parse(starUIs) as UIElement : undefined, [starUIs])
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