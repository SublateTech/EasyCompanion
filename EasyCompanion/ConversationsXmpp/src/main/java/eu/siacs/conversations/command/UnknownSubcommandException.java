/*
    This file is part of Project MAXS.

    MAXS and its modules is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    MAXS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MAXS.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.siacs.conversations.command;

@SuppressWarnings("serial")
public class UnknownSubcommandException extends IllegalStateException {

	public UnknownSubcommandException(Command command) {
		super("Unknown subcommand cmd=" + command.getCommand() + " subCmd="
				+ command.getSubCommand());
	}
}
