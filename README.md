<p align="center">
  <img width="300" alt="logo" src="https://user-images.githubusercontent.com/13380182/140794373-b357f431-d86b-421c-aca7-0102d85e1bc8.png" />
  <br/>
  <p align="center">This repository provides a desktop application to remotely manage OSGi frameworks</p>
</p>
<p align="center">
  <a href="https://github.com/amitjoy/osgifx-console"><img width="170" alt="logo" src="https://img.shields.io/static/v1?label=amitjoy&message=osgi-fx-console&color=blue&logo=github" /></a>
  <a href="https://github.com/amitjoy/osgifx-console"><img width="80" alt="logo" src="https://img.shields.io/github/stars/amitjoy/osgifx-console?style=social" /></a>
  <a href="https://github.com/amitjoy/osgifx-console"><img width="80" alt="logo" src="https://img.shields.io/github/forks/amitjoy/osgi-messaging?style=social" /></a>
  <a href="#license"><img width="110" alt="logo" src="https://img.shields.io/badge/License-Apache-blue" /></a>
  <a href="https://github.com/amitjoy/osgifx-console/runs/1485969918"><img width="95" alt="logo" src="https://img.shields.io/badge/Build-Passing-brightgreen" /></a>
  <a href="https://github.com/amitjoy/osgifx-console/releases/"><img width="134" alt="logo" src="https://img.shields.io/github/release/amitjoy/osgifx-console?include_prereleases&sort=semver" /></a>
  </p>

![1](https://user-images.githubusercontent.com/13380182/140800774-a95ccb9c-217a-4bb3-94e1-56772bf364cb.png)
![2](https://user-images.githubusercontent.com/13380182/140800782-a413fcaf-8e1b-44f1-a39d-4464e2790164.png)
![3](https://user-images.githubusercontent.com/13380182/140800788-14a18d50-0f64-4704-b217-5c9bdb8efa9b.png)
![4](https://user-images.githubusercontent.com/13380182/140800795-ab1dca5f-d05b-47a6-9fc4-2c74cc9b85fe.png)

------------------------------------------------------------------------------------------------------------

### Tools and Technologies

|                      	|                                             	|
|----------------------	|---------------------------------------------	|
| Java                 	| 1.8                                         	|
| Rich Client Platform 	| JavaFX 8                                    	|
| Runtime Frameworks   	| OSGi (Equinox), Eclipse 4 (e4), e(fx)clipse 	|
| UI Libraries         	| ControlsFX, TilesFX, FormsFX                  |
| Tools                	| Bndtools 6                                  	|

------------------------------------------------------------------------------------------------------------

### Features

|                                                                                                                                                                         	|   	|
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------	|:-:	|
| List all installed bundles and fragments                                                                                                                                        	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List all exported and imported packages                                                                                                                                       	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List all registered services                                                                                                                                                    	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List all registered DS components                                                                                                                                               	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List all available system and framework properties                                                                                                                              	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List all daemon and non-daemon threads                                                                                                                                          	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Receive events on demand (option to start and stop receiving events)                                                                                                            	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Execute Gogo command                                                                                                                                                            	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Auto-completion of all available remote Gogo commands during command execution                                                                                                    |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Install or update bundle                                                                                                                                                        	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Drag and drop support of bundles (on Install Bundle Dialog) while installing or updating                                                                                        	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List available configurations from `ConfigurationAdmin`                                                                                                                             |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List property descriptors (`Metatype`)                                                                                                                                            |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Start/stop/uninstall bundle or fragment                                                                                                                                         	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Enable/disable DS component                                                                                                                                                     	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Update/delete existing configuration                                                                                                                                            	 |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	 |
| Create new configuration using metatype descriptor                                                                                                                              	 |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	 |
| Overview of the remote OSGi framework (memory consumption, uptime, framework information, number of bundles, number of threads, number of services and number of DS components) 	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|

--------------------------------------------------------------------------------------------------------------

### Minimum Requirements for Runtime Agent

1. Java 8
2. OSGi R6

------------------------------------------------------------------------------------------------------------

### Installation of Agent

To use the agent in the OSGi environment, you need to install `in.bytehue.osgifx.console.agent` and set `osgi.fx.agent.port` system property in the runtime

--------------------------------------------------------------------------------------------------------------

### Project Import for Development

1. Install Bndtools from Eclipse Marketplace
2. Import all the projects (`File -> Import -> General -> Existing Projects into Workspace` and select `Search for nested projects`)

--------------------------------------------------------------------------------------------------------------

### Building from Source

Run `./gradlew clean build` in the project root directory

--------------------------------------------------------------------------------------------------------------

### Developer

Amit Kumar Mondal (admin@amitinside.com)

--------------------------------------------------------------------------------------------------------------

### Contribution [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/osgifx-console/issues)

Want to contribute? Great! Check out [Contribution Guide](https://github.com/amitjoy/osgifx-console/blob/main/CONTRIBUTING.md)

--------------------------------------------------------------------------------------------------------------

### License

This project is licensed under Apache License Version 2.0 [![License](http://img.shields.io/badge/license-Apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
