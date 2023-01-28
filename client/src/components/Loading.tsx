import {useEffect} from "react"
import './Loading.css'
import {Assets} from '@pixi/assets'
import {delay} from "../utils"
import {useAppDispatch} from "../storeHook"
import {addAssets} from "../stores/gameState"
import {manifest} from "../manifest"
import {setPage$} from "../dbus";

export default function Loading() {
    const dispatch = useAppDispatch()
    useEffect(() => {
        (async () => {
            await Assets.init({manifest})
            await Promise.all(manifest.bundles.map(async bundle => {
                const assets = await Assets.loadBundle(bundle.name)
                dispatch(addAssets({
                    name: bundle.name,
                    bundle: assets
                }))
            }))
            await delay(2000)
            setPage$.next('login')
        })()
    }, [])
    return (
        <>
            <svg className="svg" xmlns="http://www.w3.org/2000/svg" width="44" height="44" viewBox="0 0 44 44">
                <g fill="none" fillRule="evenodd" strokeWidth="2">
                    <circle cx="22" cy="22" r="1">
                        <animate attributeName="r" begin="0s" dur="1.8s" values="1; 20" calcMode="spline"
                                 keyTimes="0; 1" keySplines="0.165, 0.84, 0.44, 1" repeatCount="indefinite"/>
                        <animate attributeName="stroke-opacity" begin="0s" dur="1.8s" values="1; 0" calcMode="spline"
                                 keyTimes="0; 1" keySplines="0.3, 0.61, 0.355, 1" repeatCount="indefinite"/>
                    </circle>
                    <circle cx="22" cy="22" r="1">
                        <animate attributeName="r" begin="-0.9s" dur="1.8s" values="1; 20" calcMode="spline"
                                 keyTimes="0; 1" keySplines="0.165, 0.84, 0.44, 1" repeatCount="indefinite"/>
                        <animate attributeName="stroke-opacity" begin="-0.9s" dur="1.8s" values="1; 0"
                                 calcMode="spline" keyTimes="0; 1" keySplines="0.3, 0.61, 0.355, 1"
                                 repeatCount="indefinite"/>
                    </circle>
                </g>
            </svg>
            <div>Loading...</div>
        </>
    )
}