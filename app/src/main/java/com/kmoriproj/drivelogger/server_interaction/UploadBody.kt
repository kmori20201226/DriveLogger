package com.kmoriproj.drivelogger.server_interaction

import com.kmoriproj.drivelogger.db.Trajectory
import com.kmoriproj.drivelogger.db.Trip

data class UploadBody(
    val trip: Trip,
    val trajectories: List<Trajectory>
) {
}