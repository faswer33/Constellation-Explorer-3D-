#🌌 Constellation Explorer (3D) — Multi‑Language Interactive Star Map
8 languages, one immersive 3D constellation viewer – explore the night sky from any angle, rotate and zoom, and discover the stars in a whole new dimension – right from your terminal.

✨ Features
🌟 3D star map – view constellations in a 3D perspective with depth

🔄 Rotate – change azimuth and elevation angles to see constellations from different angles

🔍 Zoom – adjust scale to see more or less of the sky

📋 List constellations – see all available constellations

📖 Detailed info – mythology, brightest stars, and notable objects

📐 Projection – orthographic or perspective (configurable)

🎨 ASCII rendering – clean terminal output with stars and connecting lines

🧰 Supported Languages & Files
Language	File	Dependencies
Python	constellation_3d.py	none (stdlib)
Go	constellation_3d.go	none (stdlib)
JavaScript (Node)	constellation_3d.js	commander (optional)
Ruby	constellation_3d.rb	json, optparse
PHP	constellation_3d.php	none (extensions)
Java	Constellation3D.java	Java 8+ (Gson for JSON)
C#	Constellation3D.cs	.NET Core 3.1+
C++	constellation_3d.cpp	nlohmann/json
🚀 Quick Start
All implementations share the same CLI pattern:

bash
# Show a 3D view of a constellation with default angles
<command> view Orion

# Rotate view (azimuth and elevation in degrees)
<command> view Orion --az 45 --el 30

# Zoom in/out (scale factor)
<command> view Orion --scale 2.0

# List all constellations
<command> list

# Show detailed info about a constellation
<command> info Orion
Arguments:

view <constellation> – display 3D ASCII view

--az <degrees> – azimuth angle (rotation around vertical axis)

--el <degrees> – elevation angle (tilt up/down)

--scale <factor> – zoom scale (default: 1.0)

--projection <ortho|persp> – projection type (default: ortho)

list – show all available constellations

info <constellation> – show detailed information

📸 Example Output
text
🌟 Constellation: Orion (3D View)
Azimuth: 45°  Elevation: 30°  Scale: 1.0

                     *
                    / \
                   /   \
                  /     *
                 /     / \
                /     /   *
               /     /   / \
              /     /   /   *
             /     /   /   / \
            /     /   /   /   *
           /     /   /   /   /
          /     /   /   /   /
         /     /   /   /   /
        /     /   /   /   /
       *-----*---*---*---*
(Actual output shows a 3D projection with stars and connecting lines)

📁 Repository Structure
text
.
├── README.md
├── python/
│   └── constellation_3d.py
├── go/
│   └── constellation_3d.go
├── javascript/
│   └── constellation_3d.js
├── ruby/
│   └── constellation_3d.rb
├── php/
│   └── constellation_3d.php
├── java/
│   └── Constellation3D.java
├── csharp/
│   └── Constellation3D.cs
└── cpp/
    └── constellation_3d.cpp
