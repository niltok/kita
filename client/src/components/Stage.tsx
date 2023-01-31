import {ReactReduxContext} from "react-redux"
import {_ReactPixi, Stage as PixiStage} from '@inlet/react-pixi'
import React, {useEffect} from "react"
import {useAppDispatch, useAppSelector} from "../storeHook";
import {diffGame} from "../stores/gameState";
import {keyEvents$} from "../dbus";

export const Stage = ({children, ...props}: _ReactPixi.IStage) => {
    const {height, width} = useAppSelector(state => state.gameState.windowSize)
    const dispatch = useAppDispatch()
    useEffect(() => {
        const listener = () => {
            dispatch(diffGame({
                windowSize: {
                    height: document.body.clientHeight,
                    width: document.body.clientWidth
                }
            }))
        }
        window.addEventListener('resize', listener)
        return () => window.removeEventListener('resize', listener)
    }, [])
    useEffect(() => {
        function handleKeyEvent(e: KeyboardEvent) {
            e.preventDefault()
            if (e.repeat) return
            keyEvents$.next(e)
        }
        document.body.addEventListener('keydown', handleKeyEvent)
        document.body.addEventListener('keyup', handleKeyEvent)
        return () => {
            document.body.removeEventListener('keydown', handleKeyEvent)
            document.body.removeEventListener('keyup', handleKeyEvent)
        }
    }, [])
    return (
        <ReactReduxContext.Consumer>
            {value => (
                <PixiStage {...props} height={height} width={width}>
                    <React.StrictMode>
                        <ReactReduxContext.Provider value={value}>
                            {children}
                        </ReactReduxContext.Provider>
                    </React.StrictMode>
                </PixiStage>
            )}
        </ReactReduxContext.Consumer>
    )
}