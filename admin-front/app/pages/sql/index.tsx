/**
 * Redirect to Datasource(Default)
 */

import { Navigate } from "react-router";

export default function Home() {
	return <Navigate to="datasource" replace />;
}
