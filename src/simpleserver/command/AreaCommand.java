/*
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package simpleserver.command;

import simpleserver.Player;

public class AreaCommand extends AbstractCommand implements PlayerCommand {
  public AreaCommand() {
    super("area",
          "Use once to start a protect area. Use again to complete the protect area.");
  }

  public void execute(Player player, String message) {
    String[] arguments = extractArguments(message);
    boolean tall = false;
    if (arguments.length > 0) {
      if (arguments[0].equals("tall")) {
        tall = true;
      }
    }
    System.out.println("X: " + player.getX() + " Y: " + player.getY() + " Z: " + player.getZ());
    System.out.println("X: " + (int)player.getX() + " Y: " + (int)player.getY() + " Z: " + (int)player.getZ());
    int ret = player.getServer().areas.createArea(player.getName(),
                                                  (int) player.getX(),
                                                  (int) player.getY(),
                                                  (int) player.getZ(), false,
                                                  player.getGroupId(), tall);
    switch (ret) {
      case 1:
        player.addMessage("Started setting the area... use the command again to finish setting area.");
        break;
      case 2:
        player.addMessage("Area created!");
        break;
      default:
        player.addMessage("Error " + ret + "!");
        break;
    }
  }
}
