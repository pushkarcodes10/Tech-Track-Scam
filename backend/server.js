const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const { Parser } = require('json2csv');

const app = express();
const PORT = 3000;

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Store for active users and session history
const activeUsers = {};
const sessionHistory = [];

// When app connects and user starts tracking
app.post('/connect', (req, res) => {
    const { username } = req.body;
    if (!username) {
        return res.status(400).json({ error: 'Username is required' });
    }
    console.log(`[CONNECT] User started tracking: ${username}`);
    
    // Record start time
    activeUsers[username] = { startTime: Date.now() };
    
    res.json({ message: 'Connected successfully', username });
});

// Admin endpoint to stop all active sessions and finalize times
app.post('/stopAll', (req, res) => {
    const now = Date.now();
    let stoppedCount = 0;
    
    for (const [username, data] of Object.entries(activeUsers)) {
        const activeTimeMillis = now - data.startTime;
        
        sessionHistory.push({
            username,
            activeTimeMillis,
            timestamp: now
        });
        
        delete activeUsers[username];
        stoppedCount++;
    }
    
    console.log(`[STOP ALL] Stopped ${stoppedCount} active sessions.`);
    res.json({ message: `Successfully stopped ${stoppedCount} sessions.` });
});

// API to get dashboard data
app.get('/api/dashboard', (req, res) => {
    res.json({
        activeUsers,
        sessionHistory
    });
});

// Export CSV
app.get('/export', (req, res) => {
    try {
        const data = sessionHistory.map(session => ({
            Username: session.username,
            'Active Time (ms)': session.activeTimeMillis,
            'Active Time (Formatted)': formatHms(session.activeTimeMillis),
            'Date': new Date(session.timestamp).toLocaleString()
        }));

        if (data.length === 0) {
            return res.status(400).send('No data available to export');
        }

        const json2csvParser = new Parser();
        const csv = json2csvParser.parse(data);

        res.header('Content-Type', 'text/csv');
        res.attachment('usage_report.csv');
        res.send(csv);
    } catch (err) {
        console.error(err);
        res.status(500).send('Error generating CSV');
    }
});

// Utility function to format millis
function formatHms(millis) {
    const totalSecs = Math.floor(millis / 1000);
    const h = Math.floor(totalSecs / 3600);
    const m = Math.floor((totalSecs % 3600) / 60);
    const s = totalSecs % 60;
    
    if (h > 0) return `${h}h ${m}m ${s}s`;
    if (m > 0) return `${m}m ${s}s`;
    return `${s}s`;
}

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server is bound to 0.0.0.0 and listening on port ${PORT}`);
    console.log(`You can view the dashboard locally at: http://localhost:${PORT}`);
});
