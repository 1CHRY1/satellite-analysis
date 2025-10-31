/**
 * Redirect to Backend(Default)
 */

import { Navigate } from "react-router";

export default function Home() {
	return <Navigate to="backend" replace />;
}
