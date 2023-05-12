import {UIElement} from "../types/UIElement";
import {sendSocket$} from "../dbus";
import {store} from "../store";
import {useDiffGame} from "../stores/gameState";
import React, {useEffect} from "react";
import {useAppSelector} from "../storeHook";

export function RenderUI(prop: {elem: UIElement | undefined}) {
    const elem = prop.elem
    const uiState = useAppSelector(state => state.gameState.uiState)
    const diffGame = useDiffGame()
    useEffect(() => {
        if (elem && elem.stateName) {
            console.log('reset', elem.stateName, elem.value)
            diffGame({uiState: {[elem.stateName]: elem.value ?? ''}})
        }
    }, [elem?.value])
    if (!elem) return <></>
    const commonProp = {
        style: JSON.parse(elem.style) ?? {},
        className: elem.classes.join(' ') ?? "",
        children: elem.children.length == 0 ? undefined : elem.children.filter(e => e).map(e => <RenderUI elem={e}/>),
        title: elem.title ?? undefined
    }
    const clickCallback = elem.callback && elem.callback != "{}" ? {
        onClick: () => {
            let states: {[key: string]: any} = {}
            if (elem.states) elem.states.forEach(s => states[s] = store.getState().gameState.uiState[s])
            sendSocket$.next({
                ...JSON.parse(elem.callback as string),
                states
            })
        }
    } : {}
    const inputProp = elem.stateName ? {
        value: uiState[elem.stateName],
        onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
            console.log(elem.stateName, e.target.value)
            if (elem.stateName) diffGame({uiState: {[elem.stateName]: e.target.value}})
        }
    } : {}
    switch (elem.type) {
        case "div":
            return (<div {...commonProp} {...clickCallback} />)
        case "span":
            return (<span {...commonProp} {...clickCallback} />)
        case "button":
            return (<button {...commonProp} {...clickCallback} />)
        case "input.text":
            return (<input type={"text"} {...commonProp} {...inputProp} />)
        case "input.number":
            return (<input type={"number"} {...commonProp} {...inputProp} />)
        case "text":
            return <>{elem.text}</>
        case "br":
            return <br/>
        default:
            return <>{elem.type}</>
    }
}