Consider this scenario (SV stands for Stateless Verticle). You have SV1 that's going to call SV2, SV3, SV4. SV1 is effectively blocked until those calls are completed, even if the event loop thread isn't blocked. So you can easily have more instances of a SV than you have event loop threads. That's why we need a way of specifying the number to deploy dynamically - kind of have this already with the game library deployer. 

And then build the tool that allows dynamic redeployment - but with the ability to keep "things" around - named pools of things for example.
