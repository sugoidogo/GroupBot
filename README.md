# studious-potato

The goal of this project is to provie non-admin discord server members to create and join roles with the explicit purpose of pinging a group when a message is relevant to them.

Any server member may create a role via the bot. The role is not granted any special permissions. Any server member may then join that role, to be notified whenever that role is pinged.

The bot will use a channel called 'groups'. This channel should be empty of messages, except for groups.
A group is defined by a message in the groups channel. The group name is the first word of the message.
GroupBot will add a reaction to the message. Any other reactions in this channel will be removed to avoid confusion. If the bot encountered an error while making the group, the reaction will be an x. Usually this means the name cannot be used. You will need to delete the message and send a new one with a valid role name.

To join a group and get pinged with it, simply click the reaction. GroupBot will add you to the role.

For example, in a server there may be many members who play a game. If a memeber wants to announce a rare in-game item becoming temporarily availible, rather than pinging those memebers that he knows play said game individually, he may ping the role for that game and all members who have joined that role will receive the ping, making sending and receiving these messages easier.

Implementation is in my Nerd Bot: https://discordapp.com/oauth2/authorize?client_id=340782381139230720&scope=bot&permissions=268501056

You can also run this on your own bot. Use your bot token as the only arg when launching the program.
