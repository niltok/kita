import React from 'react'
import ReactDOM from 'react-dom'
import App from './components/App'
import './index.css'
import {ErrorBoundary, FallbackProps} from "react-error-boundary"
import {Provider} from "react-redux"
import {store} from "./store"
import {enableMapSet} from 'immer'

export function ErrorFallback({error, resetErrorBoundary}: FallbackProps) {
    return (
        <div role="alert">
            <p>Something went wrong:</p>
            <pre>{error.message}</pre>
            <button onClick={resetErrorBoundary}>Try again</button>
        </div>
    )
}

//enableMapSet()

ReactDOM.render(
    <React.StrictMode>
        <ErrorBoundary FallbackComponent={ErrorFallback}>
            <Provider store={store}>
                <App/>
            </Provider>
        </ErrorBoundary>
    </React.StrictMode>,
    document.getElementById('app-root') as HTMLElement
)
