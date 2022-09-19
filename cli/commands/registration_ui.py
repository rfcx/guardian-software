import tkinter as tk
from commands.common.registration_command import *

class RemoveRegistrationPage(tk.Frame):

   def __init__(self, parent, controller):
      tk.Frame.__init__(self, parent)
      self.device = controller.getDevice()

      self.columnconfigure([0, 1, 2, 3, 4, 5], weight = 1)
      label = tk.Label(self, text = "Registration", font = ("Courier", 14), anchor = "center")
      label.grid(row = 0, column = 0, sticky = "nsew", columnspan = 5)

      self.registrationStatus = tk.Label(self, text = "Status: ", font = ("Courier", 14), anchor = "center")
      self.registrationStatus.grid(row = 1, column = 0, sticky = "nsew", columnspan = 5)


      removeButton = tk.Button(self, text = "Remove", fg = "white", bg = "red", width = 50, height = 2, anchor = "center", command = lambda: self.remove())
      removeButton.grid(row = 2, column = 0, sticky = "nsew", columnspan = 5)

      self.refresh()

   def refresh(self):
      self.registrationStatus.config(text = f"Status: {isRegistered(self.device)}")

   def remove(self):
      removeRegistration(self.device)
      self.refresh()