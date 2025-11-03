import { createContext, useReducer, type Dispatch, type JSX } from "react";

interface TestState {
    test: string
}

interface TestAction {
    type: "LOGIN" | "LOGOUT"
    payload: any
}

export const TestContext = createContext<{state: TestState, dispatch: Dispatch<TestAction>} | undefined>(undefined)

export default function TestProvider ({children}: {children: React.ReactNode}){
    const reducer = (prev: TestState, action: TestAction) => {
        switch (action.type) {
            case "LOGIN":
                return {
                    ...prev,
                }
            case "LOGOUT":
                return {
                    ...prev,
                    test: ""
                }
            default:
                return prev
        }
    }

    const [state, dispatch] = useReducer(reducer, {test: ''})

    return (
        <TestContext.Provider value={{state, dispatch}}>
            {children}
        </TestContext.Provider>
    )
}