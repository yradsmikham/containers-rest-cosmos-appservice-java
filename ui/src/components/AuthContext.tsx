import { createContext } from 'react'

export interface IAuthContextValues {
  accessToken?: string,
  authResponse?: string,
  handleAuth?: () => any,
  setAuthResponse?: (msg?: string) => any,
}

// Default values and enforce use of interface
const defaultAuthContextValue: IAuthContextValues = {}

export const AuthContext = createContext<IAuthContextValues>(defaultAuthContextValue)
// exporting the Provider and Consumer components for more specific imports
export const AuthProvider = AuthContext.Provider
export const AuthConsumer = AuthContext.Consumer
