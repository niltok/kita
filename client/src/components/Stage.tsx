import {ReactReduxContext} from "react-redux"
import {_ReactPixi, Container, Stage as PixiStage} from '@inlet/react-pixi'
import React, {useEffect, useState} from "react"

export const Stage = ({children, ...props}: _ReactPixi.IStage) => {
    const [height, setHeight] = useState(document.body.clientHeight)
    const [width, setWidth] = useState(document.body.clientWidth)
    useEffect(() => {
        const listener = () => {
            setHeight(document.body.clientHeight)
            setWidth(document.body.clientWidth)
        }
        window.addEventListener('resize', listener)
        return () => window.removeEventListener('resize', listener)
    })
    return (
        <ReactReduxContext.Consumer>
            {value => (
                <PixiStage {...props} height={height} width={width}>
                    <React.StrictMode>
                        <ReactReduxContext.Provider value={value}>
                            <Container position={[width / 2, height / 2]}>
                                {children}
                            </Container>
                        </ReactReduxContext.Provider>
                    </React.StrictMode>
                </PixiStage>
            )}
        </ReactReduxContext.Consumer>
    )
}