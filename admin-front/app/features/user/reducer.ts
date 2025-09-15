import type { UserState, UserAction } from "~/types/user";


export function userReducer(state: UserState, action: UserAction): UserState {
	switch (action.type) {
		case "LOGIN":
			return { authenticated: true, user: action.payload };
		case "LOGOUT":
			return {
				authenticated: false,
				user: {
					id: "",
					name: "",
					phone: "",
					province: "",
					city: "",
					email: "",
					title: "",
					organization: "",
					introduction: "",
					roleId: 2,
					roleName: "",
					roleDesc: "",
					maxCpu: 2,
					maxStorage: 5,
					maxJob: 3,
					isSuperAdmin: false,
				},
			};
        default:
            return state
	}
}
