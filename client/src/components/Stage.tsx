import {ReactReduxContext} from "react-redux"
import {_ReactPixi, Stage as PixiStage} from '@inlet/react-pixi'
import React, {useEffect} from "react"
import {useAppDispatch, useAppSelector} from "../storeHook";
import {diffGame} from "../stores/gameState";

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