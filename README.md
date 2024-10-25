# MCMidi

This mod allows you to play midi files.

## Commands
- `/midi play [targets] [path_to_midi(Optional)] [loopCount(Optional)] [startTick(Optional)]`

The midi files is located to `$ClientPath/midi/mcmidi/midi/*.midi`
If the command run, server will send a midi file to your client and play it in client.

- `/midi stop [targets]`

This command requests to client to stop midi playing.

### Example

![Midi Controll Center](https://cdn.modrinth.com/data/cached_images/6e5e86cb826a3ff08c17d68872e0413eb5bbdab9_0.webp)

## KeyBinds
`,`:Open midi controll center screen.

## Other changes
This mod adds gameoptions screen likes this image.
![Options screen](https://cdn.modrinth.com/data/cached_images/1aa9aa4b569dd9bc73db032c5731ab674fd75ac3_0.webp)

## Supported files
- .midi
- .sf2

## Todos
- Fix momentary drop in fps when play midi files.