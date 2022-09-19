import tkinter as tk
from commands.common.internal_storage_command import *

class RemoveStoragePage(tk.Frame):

   def __init__(self, parent, controller):
      tk.Frame.__init__(self, parent)
      self.device = controller.getDevice()

      self.columnconfigure([0, 1, 2, 3, 4, 5], weight = 1)
      label = tk.Label(self, text = "Internal Storage", font = ("Courier", 14), anchor = "center")
      label.grid(row = 0, column = 0, sticky = "nsew", columnspan = 5)

      self.storage = tk.Label(self, text = "", font = ("Courier", 14), anchor = "center")
      self.storage.grid(row = 1, column = 0, sticky = "nsew", columnspan = 5)

      removeButton = tk.Button(self, text = "Remove", fg = "white", bg = "red", width = 50, height = 2, anchor = "center", command = lambda: self.remove())
      removeButton.grid(row = 2, column = 0, sticky = "nsew", columnspan = 5)

      self.refresh()

   def refresh(self):
      storage = getStorage(self.device)
      self.storage.config(text = f"{storage['free']}/{storage['all']}")

   def remove(self):
      removeStorage(self.device)
      self.refresh()