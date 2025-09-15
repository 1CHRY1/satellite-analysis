import { createContext, useContext, type Dispatch } from "react";
import type { UserState, UserAction } from "~/types/user";

export interface UserContextType {
    state: UserState
    dispatch: Dispatch<UserAction>
}

export const UserContext = createContext<UserContextType | undefined>(undefined)