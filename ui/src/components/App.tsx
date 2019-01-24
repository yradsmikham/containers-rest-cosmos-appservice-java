import { Router } from '@reach/router'
import { UserAgentApplication } from 'msal'
import * as React from 'react'

import './../styles/App.css'
import { AuthProvider } from './AuthContext'
import { AuthResponseBar } from './AuthResponseBar'
import { DefaultComponent } from './DefaultComponent'
import { Home } from './Home'
import { Navbar } from './Navbar'
import { Person } from './Person'
import { Title } from './Title'

// Webpack will automatically replace this variable during build time
const appConfig = {
    clientID: '', // defaulted to '' when no OAuth client id is passed in
}

// initialize the UserAgentApplication globally so popup and iframe can run in the background
// short circuit userAgentApp. If clientID is null so is userAgentApp
const userAgentApp = appConfig.clientID && new UserAgentApplication(appConfig.clientID, null, () => null)

// webpack replaces this variable on build time
let basepath = process.env.WEBPACK_PROP_UI_BASEPATH || '' // default to empty string for testing
const c = basepath.charAt(0)
basepath = c === '' || c === '/' || c === '\\' ? basepath : `/${basepath}`

export class App extends React.Component {

    // App is still responsible for managing the auth state
    // it uses the AuthContext to share this with other aspects of the UI
    // including the navbar, private route component, and the pages themselves
    public state = {
        accessToken: null,
        authResponse: null,
    }

    public handleAuth = async () => {
        if (appConfig.clientID === '') { // no auth flow case
            if (this.state.accessToken !== null) {
                this.setState({ accessToken: null })
            } else {
                // tslint:disable-next-line no-console max-line-length
                console.warn('AAD Client ID has not been configured. If you are currently in production mode, see the \'deploy\' documentation for details on how to fix this.')
                this.setState({ accessToken: '' })
            }
        } else if (userAgentApp === '') { // normal or 'production' auth flow
            alert('userAgentApp not initialized')
        } else {
            let accessToken = null
            try {
                if (this.state.accessToken !== null) {
                    // log out
                    if (userAgentApp) {
                        await userAgentApp.logout()
                    } else {
                        alert('userAgentApp not initialized')
                    }
                } else {
                    // log in
                    const graphScopes = [appConfig.clientID]
                    if (userAgentApp) {
                        await userAgentApp.loginPopup(graphScopes)
                        accessToken = await userAgentApp.acquireTokenSilent(graphScopes,
                            'https://login.microsoftonline.com/microsoft.onmicrosoft.com')
                    } else {
                        alert('userAgentApp not initialized')
                    }
                }
                this.setState({ accessToken })
            } catch (err) {
                this.setAuthResponse(err.toString())
            }
        }
    }

    public setAuthResponse = (msg: string) => {
        this.setState({ authResponse: msg })
    }

    public render() {
        // Implementing the authprovider at app root to share the auth state
        // with all internal components (if they subscribe to it)
        // by linking the accessToken to the app state we can be certain the
        // context will always update and propogate the value to subscribed nodes
        return (
            <AuthProvider value={{handleAuth: this.handleAuth}}>
                <div className='app-container'>
                    <Navbar basepath={basepath} />
                    <AuthResponseBar />
                    <Router basepath={basepath}>
                        <Home path='/' />
                        <Person path='/people' />
                        <Title path='/titles' />
                        <DefaultComponent default />
                    </Router>
                </div>
            </AuthProvider>
        )
    }
}
