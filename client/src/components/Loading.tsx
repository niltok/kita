import {useEffect} from "react"
import './Loading.css'
import {delay} from "../utils/common"
import {rendererEvent$, setPage$} from "../dbus"
import {renderer} from "../worker/workers"

export default function Loading() {
    useEffect(() => {
        (async () => {
            const promise = new Promise((res, rej) => {
                const sub = rendererEvent$.subscribe({
                    next(e) {
                        if (e.type == 'init.end') {
                            sub.unsubscribe()
                            res({})
                        }
                    }
                })
            })
            renderer.postMessage({ type: 'init' })
            await Promise.all([
                new FontFace('Sourcehanserifcn Vf.ttf', 'url(./ui/SourceHanSerifCN-VF.ttf.woff2)'),
                new FontFace('Sourcehansanscn Vf.ttf', 'url(./ui/SourceHanSansCN-VF.ttf.woff2)'),
                new FontFace('Sourcecodevf Upright.ttf', 'url(./ui/SourceCodeVF-Upright.ttf.woff2)')
            ].map(f => f.load().then(v => document.fonts.add(v))))
            await promise
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